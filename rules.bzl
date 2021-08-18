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
