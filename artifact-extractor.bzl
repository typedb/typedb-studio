bash_script_template_tar = """\
#!/bin/bash
set -ex
mkdir -p $BUILD_WORKSPACE_DIRECTORY/$1
tar -xzf {artifact_location} -C $BUILD_WORKSPACE_DIRECTORY/$1 --strip-components=2

"""

bash_script_template_unzip = """\
#!/bin/bash
set -ex
mkdir -p $BUILD_WORKSPACE_DIRECTORY/$1
tmp_dir=$(mktemp -d)
unzip -qq {artifact_location} -d $tmp_dir
mv -v $tmp_dir/{artifact_unpacked_name}/* $BUILD_WORKSPACE_DIRECTORY/$1/
rm -rf {artifact_unpacked_name}

"""

cmd_script_template_unzip = """\
set DEST_PATH=%BUILD_WORKSPACE_DIRECTORY:/=\\%\\%1
if not exist "%DEST_PATH%" mkdir %DEST_PATH%
7z x -o%DEST_PATH%\\ {artifact_location}
robocopy %DEST_PATH%\\{artifact_unpacked_name} %DEST_PATH% /E /MOVE

"""

def _artifact_extractor_impl(ctx):
    supported_extensions_map = {
        'zip': {
            'cmd': cmd_script_template_unzip,
            'bash': bash_script_template_unzip,
        },
        'tar': {
            'bash': bash_script_template_tar
        },
        'tar.gz': {
            'bash': bash_script_template_tar
        },
        'tgz': {
            'bash': bash_script_template_tar
        },
        'taz': {
            'bash': bash_script_template_tar
        },
        'tar.bz2': {
            'bash': bash_script_template_tar
        },
        'tb2': {
            'bash': bash_script_template_tar
        },
        'tbz': {
            'bash': bash_script_template_tar
        },
        'tbz2': {
            'bash': bash_script_template_tar
        },
        'tz2': {
            'bash': bash_script_template_tar
        },
        'tar.lz': {
            'bash': bash_script_template_tar
        },
        'tar.lzma': {
            'bash': bash_script_template_tar
        },
        'tlz': {
            'bash': bash_script_template_tar
        },
        'tar.lzo': {
            'bash': bash_script_template_tar
        },
        'tar.xz': {
            'bash': bash_script_template_tar
        },
        'txz': {
            'bash': bash_script_template_tar
        },
        'tar.Z': {
            'bash': bash_script_template_tar
        },
        'tar.zst': {
            'bash': bash_script_template_tar
        }
    }

    artifact_file = ctx.file.artifact
    artifact_filename = artifact_file.basename
    extraction_method = ctx.attr.extraction_method
    executor = ctx.attr.executor

    if (extraction_method == 'auto'):
        artifact_extention = None
        for ext in supported_extensions_map.keys():
            if artifact_filename.rfind(ext) == len(artifact_filename) - len(ext):
                artifact_extention = ext
                target_script_template = supported_extensions_map.get(ext).get(executor)
                artifact_unpacked_name = artifact_filename.replace('.' + ext, '')
                break
        
        if artifact_extention == None:
            fail("Extention [{extention}] is not supported by the artifiact_etractor.".format(extention = artifact_file.extension))
    elif (extraction_method == 'tar'):
        target_script_template = supported_extensions_map.get('tar').get(executor)
    elif (extraction_method == 'unzip'):
        target_script_template = supported_extensions_map.get('zip').get(executor)

    if target_script_template == None:
        fail('Extracting a [{extension}] with [{executor}] is not yet supported by artifact-extractor.'.format(extension=artifact_extention, executor=executor))

    extensions_map = {
        'bash': 'sh',
        'cmd': 'bat'
    }

    artifact_location = artifact_file.short_path
    if executor == 'cmd': artifact_location = artifact_location.replace('/', '\\')

    # Emit the executable shell script.
    script = ctx.actions.declare_file("{name}.{ext}".format(name=ctx.label.name, ext=extensions_map.get(executor)))
    script_content = target_script_template.format(
        artifact_location = artifact_location,
        artifact_unpacked_name = artifact_unpacked_name
    )

    ctx.actions.write(script, script_content, is_executable = True)

    # The datafile must be in the runfiles for the executable to see it.
    runfiles = ctx.runfiles(files = [artifact_file])
    return [DefaultInfo(executable = script, runfiles = runfiles)]

artifact_extractor = rule(
    implementation = _artifact_extractor_impl,
    attrs = {
        "artifact": attr.label(
            mandatory = True,
            allow_single_file = True,
            doc = "Artifact archive to extract.",
        ),
        "extraction_method": attr.string(
            default = "auto",
            values = ["unzip", "tar", "auto"],
            doc = "the method to use for extracting the artifact."
        ),
        "executor": attr.string(
            default = "bash",
            values = ["bash", "cmd"],
            doc = "the executor to use for running the extraction script."
        )
    },
    executable = True,
)
