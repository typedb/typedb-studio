script_template_tar = """\
#!/bin/bash
set -ex
mkdir -p $BUILD_WORKSPACE_DIRECTORY/$1
tar -xzf {artifact_location} -C $BUILD_WORKSPACE_DIRECTORY/$1 --strip-components=2
"""

script_template_unzip = """\
#!/bin/bash
set -ex
mkdir -p $BUILD_WORKSPACE_DIRECTORY/$1
tmp_dir=$(mktemp -d)
unzip -qq {artifact_location} -d $tmp_dir
mv -v $tmp_dir/{artifact_unpacked_name}/* $BUILD_WORKSPACE_DIRECTORY/$1/
rm -rf {artifact_unpacked_name}
"""

def _artifact_extractor_impl(ctx):
    supported_extensions_script_map = {
        'zip': script_template_unzip,
        'tar.gz': script_template_tar,
        'tgz': script_template_tar,
        'taz': script_template_tar,
        'tar.bz2': script_template_tar,
        'tb2': script_template_tar,
        'tbz': script_template_tar,
        'tbz2': script_template_tar,
        'tz2': script_template_tar,
        'tar.lz': script_template_tar,
        'tar.lzma': script_template_tar,
        'tlz': script_template_tar,
        'tar.lzo': script_template_tar,
        'tar.xz': script_template_tar,
        'txz': script_template_tar,
        'tar.Z': script_template_tar,
        'tar.zst': script_template_tar,
    }

    artifact_file = ctx.file.artifact
    artifact_filename = artifact_file.basename

    extraction_method = ctx.attr.extraction_method

    if (extraction_method == 'auto'):
        artifact_extention = None
        for ext in supported_extensions_script_map.keys():
            if artifact_filename.rfind(ext) == len(artifact_filename) - len(ext):
                artifact_extention = ext
                target_script_template = supported_extensions_script_map.get(ext)
                artifact_unpacked_name = artifact_filename.replace('.' + ext, '')
                break
        
        if artifact_extention == None:
            fail("Extention [{extention}] is not supported by the artifiact_etractor.".format(extention = artifact_file.extension))
    elif (extraction_method == 'tar'):
        target_script_template = script_template_tar
    elif (extraction_method == 'unzip'):
        target_script_template = script_template_unzip

    # Emit the executable shell script.
    script = ctx.actions.declare_file("%s.sh" % ctx.label.name)
    script_content = target_script_template.format(
        artifact_location = artifact_file.short_path,
        artifact_unpacked_name = artifact_unpacked_name
    )

    ctx.actions.write(script, script_content, is_executable = True)

    # The datafile must be in the runfiles for the executable to see it.
    runfiles = ctx.runfiles(files = [artifact_file])
    return [DefaultInfo(executable = script, runfiles = runfiles)]

_artifact_extractor = rule(
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
        )
    },
    executable = True,
)

def artifact_extractor(
        name,
        artifact,
        extraction_method = "auto",
    ):
    
    extractor_name = "{}_extract__do_not_reference".format(name)
    
    _artifact_extractor(
        name = extractor_name,
        artifact = artifact,
        extraction_method = extraction_method,
    )

    native.sh_binary(
        name = name,
        srcs = [extractor_name],
    )