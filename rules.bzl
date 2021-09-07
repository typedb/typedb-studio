load("@vaticle_bazel_distribution//common:rules.bzl", "assemble_zip", "java_deps")

def _print_rootpath_impl(ctx):
    ctx.actions.write(
        output = ctx.outputs.executable,
        content = "echo '%s/%s'" % (ctx.files.target[0].root.path, ctx.files.target[0].path),
        is_executable = True
    )

    return [DefaultInfo(
        runfiles = ctx.runfiles()
    )]

print_rootpath = rule(
    attrs = {
        "target": attr.label(
            allow_single_file = True,
            mandatory = True,
            doc = "The target",
        ),
        "out": attr.label(
            allow_single_file = True,
            mandatory = True,
            doc = "The output file",
        ),
    },
    implementation = _print_rootpath_impl,
    doc = "Print rootpath",
    executable = True,
)


def _jpackage_impl(ctx):
    ctx.actions.run(
        inputs = [ctx.file.src],
        outputs = [ctx.outputs.distribution_file],
        executable = ctx.executable._jpackage_runner_kt,
        arguments = [
            ctx.file.src.path,
            ctx.attr.application_name,
            ctx.attr.main_jar,
            ctx.attr.main_class,
            ctx.outputs.distribution_file.path,
        ],
        progress_message = "Building native {} application image".format(ctx.attr.application_name)
    )

    return DefaultInfo(data_runfiles = ctx.runfiles(files=[ctx.outputs.distribution_file]))

jpackage = rule(
    attrs = {
        "src": attr.label(
            mandatory = True,
            allow_single_file = True,
            doc = "The source",
        ),
        "application_name": attr.string(
            mandatory = True,
            doc = "The application name",
        ),
        "main_jar": attr.string(
            mandatory = True,
            doc = "The name of the JAR containing the main method",
        ),
        "main_class": attr.string(
            mandatory = True,
            doc = "The main class",
        ),
        "_jpackage_runner_kt": attr.label(
            default = "//:jpackage-runner",
            executable = True,
            cfg = "host",
        ),
    },
    outputs = {
        "distribution_file": "%{name}.tar.gz"
    },
    implementation = _jpackage_impl,
    doc = "A JVM application image",
)


def jvm_application_image(name,
                          application_name,
                          jvm_binary,
                          main_jar,
                          main_class,
                          deps_use_maven_name = True,
                          additional_files = {}):

    # TODO: need native libraries deps
    java_deps(
        name = "{}-deps-native".format(name),
        target = jvm_binary,
        java_deps_root = "lib/",
        maven_name = deps_use_maven_name,
    )

    assemble_filename = "{}-assemble".format(name)

    assemble_zip(
        name = "{}-assemble-native-zip".format(name),
        targets = [":{}-deps-native".format(name)],
        additional_files = additional_files,
        output_filename = assemble_filename,
    )

    jpackage(
        name = name,
        src = ":{}-assemble-native-zip".format(name),
        application_name = application_name,
        main_jar = "lib/" + main_jar,
        main_class = main_class,
    )
