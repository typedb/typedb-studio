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
mkdir temp-artifact-extracted
7z x -otemp-artifact-extracted {artifact_location}
robocopy temp-artifact-extracted\\{artifact_unpacked_name} %DEST_PATH% /E /MOVE
rmdir /S /Q temp-artifact-extracted

"""

def _artifact_extractor_impl(ctx):
    supported_extensions_map = {
        'cmd': {
            'zip': cmd_script_template_unzip
        },
        'bash': {
            'zip': bash_script_template_unzip,
            'tar': bash_script_template_tar,
            'tar.gz': bash_script_template_tar,
            'tgz': bash_script_template_tar,
            'taz': bash_script_template_tar,
            'tar.bz2': bash_script_template_tar,
            'tb2': bash_script_template_tar,
            'tbz': bash_script_template_tar,
            'tbz2': bash_script_template_tar,
            'tz2': bash_script_template_tar,
            'tar.lz': bash_script_template_tar,
            'tar.lzma': bash_script_template_tar,
            'tlz': bash_script_template_tar,
            'tar.lzo': bash_script_template_tar,
            'tar.xz': bash_script_template_tar,
            'txz': bash_script_template_tar,
            'tar.Z': bash_script_template_tar,
            'tar.zst': bash_script_template_tar
        },
    }

    artifact_file = ctx.file.artifact
    artifact_filename = artifact_file.basename
    extraction_method = ctx.attr.extraction_method
    executor = ctx.attr.executor

    supported_extentions = []
    for executor_extentions in supported_extensions_map.values():
        for ext in executor_extentions.keys():
            if ext not in supported_extentions: supported_extentions.append(ext)

    if (extraction_method == 'auto'):
        artifact_extention = None
        for ext in supported_extentions:
            if artifact_filename.rfind(ext) == len(artifact_filename) - len(ext):
                artifact_extention = ext
                target_script_template = supported_extensions_map.get(executor).get(ext)
                artifact_unpacked_name = artifact_filename.replace('.' + ext, '')
                break
        
        if artifact_extention == None:
            fail("Extention [{extention}] is not supported by the artifiact-etractor.".format(extention = artifact_file.extension))
    elif (extraction_method == 'tar'):
        target_script_template = supported_extensions_map.get(executor).get('tar')
    elif (extraction_method == 'unzip'):
        target_script_template = supported_extensions_map.get(executor).get('zip')

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
