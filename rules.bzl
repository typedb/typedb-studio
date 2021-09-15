load("@vaticle_bazel_distribution//common:rules.bzl", "assemble_zip", "java_deps")


def _zip_to_jvm_application_image_impl(ctx):
    if (ctx.attr.os == "unknown"):
        fail("jvm_application_image is not supported on this operating system")

    # TODO: copied from bazel-distribution/pip/rules.bzl
    if not ctx.attr.version_file:
        version_file = ctx.actions.declare_file(ctx.attr.name + "__do_not_reference.version")
        version = ctx.var.get('version', '0.0.0')

        if len(version) == 40:
            # this is a commit SHA, most likely
            version = "0.0.0-{}".format(version)

        ctx.actions.run_shell(
            inputs = [],
            outputs = [version_file],
            command = "echo {} > {}".format(version, version_file.path)
        )
    else:
        version_file = ctx.file.version_file

    step_description = "Building native {} application image".format(ctx.attr.application_name)

    config = """/
verbose: {}
jdkPath: {}
srcFilename: {}
applicationName: {}
applicationFilename: {}
versionFilePath: {}
mainJar: {}
mainClass: {}
outFilename: {}
""".format(
    True,
    ctx.file.jdk.path,
    ctx.file.src.path,
    ctx.attr.application_name,
    ctx.attr.filename,
    version_file.path,
    ctx.attr.main_jar,
    ctx.attr.main_class,
    ctx.outputs.distribution_file.path)

    private_config = ""

    if "APPLE_CODE_SIGNING_CERTIFICATE_URL" in ctx.var:

        if not ctx.file.mac_entitlements:
            fail("Parameter mac_entitlements must be set if variable APPLE_CODE_SIGNING_CERTIFICATE_URL is set")

        config = config + """/
macEntitlementsPath: {}
""".format(ctx.file.mac_entitlements.path)

        private_config = private_config + """/
appleId: {}
appleIdPassword: {}
appleCodeSigningCertificateUrl: {}
appleCodeSigningPassword: {}
""".format(
        ctx.var.get("APPLEID", ""),
        ctx.var.get("APPLEID_PASSWORD", ""),
        ctx.var.get("APPLE_CODE_SIGNING_CERTIFICATE_URL", ""),
        ctx.var.get("APPLE_CODE_SIGNING_PASSWORD", ""))

        step_description = step_description + " (NOTE: notarization typically takes several minutes to complete)"

    inputs = [ctx.file.jdk, ctx.file.src]
    if ctx.file.mac_entitlements:
        inputs = inputs + [ctx.file.mac_entitlements]

    ctx.actions.run(
        inputs = inputs,
        outputs = [ctx.outputs.distribution_file],
        executable = ctx.executable._jvm_application_image_builder_bin,
        arguments = [
            config,
            private_config,
        ],
        progress_message = step_description,
    )

    return DefaultInfo(data_runfiles = ctx.runfiles(files=[ctx.outputs.distribution_file]))


zip_to_jvm_application_image = rule(
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
        "filename": attr.string(
            mandatory = True,
            doc = "The filename",
        ),
        "version_file": attr.label(
            mandatory = True,
            allow_single_file = True,
            doc = "The version file",
        ),
        "jdk": attr.label(
            allow_single_file = True,
            doc = "The JDK, which must be at least version 16",
        ),
        "main_jar": attr.string(
            mandatory = True,
            doc = "The name of the JAR containing the main method",
        ),
        "main_class": attr.string(
            mandatory = True,
            doc = "The main class",
        ),
        "os": attr.string(
            mandatory = True,
            doc = "The host OS",
        ),
        "mac_entitlements": attr.label(
            allow_single_file = True,
            doc = "The MacOS entitlements.mac.plist file",
        ),
        "_jvm_application_image_builder_bin": attr.label(
            default = "//:jvm-application-image-builder-bin",
            executable = True,
            cfg = "host",
        ),
    },
    outputs = {
        "distribution_file": "%{name}.zip"
    },
    implementation = _zip_to_jvm_application_image_impl,
    doc = "A JVM application image",
)


# TODO: upgrade all to JDK17
def native_jdk16():
    return select({
        "@vaticle_dependencies//util/platform:is_mac": "@jdk16_mac//file",
        "@vaticle_dependencies//util/platform:is_linux": "@jdk17_linux//file",
        "@vaticle_dependencies//util/platform:is_windows": "@jdk16_windows//file",
        "//conditions:default": "@jdk16_mac//file",
    })


def jvm_application_image(name,
                          application_name,
                          filename,
                          version_file,
                          jvm_binary,
                          main_jar,
                          main_class,
                          jdk = native_jdk16(),
                          deps_use_maven_name = True,
                          additional_files = {},
                          mac_entitlements = None):

    java_deps(
        name = "{}-deps".format(name),
        target = jvm_binary,
        java_deps_root = "lib/",
        maven_name = deps_use_maven_name,
    )

    assemble_filename = "{}-assemble".format(name)

    assemble_zip(
        name = "{}-assemble-zip".format(name),
        targets = [":{}-deps".format(name)],
        additional_files = additional_files,
        output_filename = assemble_filename,
    )

    zip_to_jvm_application_image(
        name = name,
        src = ":{}-assemble-zip".format(name),
        application_name = application_name,
        filename = filename,
        version_file = version_file,
        main_jar = "lib/" + main_jar,
        main_class = main_class,
        jdk = jdk,
        os = select({
            "@vaticle_dependencies//util/platform:is_mac": "mac",
            "@vaticle_dependencies//util/platform:is_linux": "linux",
            "@vaticle_dependencies//util/platform:is_windows": "windows",
            "//conditions:default": "unknown",
        }),
        mac_entitlements = mac_entitlements
    )
