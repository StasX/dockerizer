// Run from the repository root: gradle securityTest
// Or with standalone Groovy: groovy tests/DockerizerSecurityTest.groovy
List<String> commands = []
Binding binding = new Binding([
    echo: { String message -> },
    sh: { Object command -> commands.add(command.toString()) }
])
def dockerizer = new GroovyShell(binding).parse(new File('vars/dockerizer.groovy'))

['buildImage', 'tag', 'push'].each { String method ->
    ['alpine', 'team/my-app', 'team/my__app', 'team/my--app'].each { String image ->
        ['', 'dev', 'prod_1'].each { String environment ->
            commands.clear()
            dockerizer.invokeMethod(method, [image, '1.2.3', environment] as Object[])
            assert commands
            assert commands.every { it.contains("'docker.io/${image}:") }
        }
    }

    ['; touch injected', '$(id)', '`id`', "a'b", 'a"b', 'a b', 'a\nb', 'a\rb', 'a\\b'].each { String payload ->
        (0..2).each { int position ->
            List arguments = ['team/app', '1.2.3', 'dev']
            arguments[position] = payload
            commands.clear()
            try {
                dockerizer.invokeMethod(method, arguments as Object[])
                assert false: "${method} accepted unsafe argument ${position}"
            } catch (IllegalArgumentException expected) {
                assert commands.empty
            }
        }
    }

    [[null, '1', ''], ['', '1', ''], ['app', null, ''], ['app', '', ''],
     ['app', 'latest', null], ['app', 'a' * 129, ''],
     ['app', 'a' * 128, 'dev'], ['app', '1', 'a' * 122]].each { List arguments ->
        commands.clear()
        try {
            dockerizer.invokeMethod(method, arguments as Object[])
            assert false: "${method} accepted invalid arguments"
        } catch (IllegalArgumentException expected) {
            assert commands.empty
        }
    }

    dockerizer.invokeMethod(method, ['app', 'a' * 128, ''] as Object[])
    dockerizer.invokeMethod(method, ['app', 'latest', 'dev'] as Object[])
}

println 'Dockerizer security checks passed'
