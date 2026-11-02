def buildImage(
    String image,
    String version = 'latest',
    String envName = ''
) {
    validateArguments(image, version, envName)
    echo 'Building Docker image...'

    if (!envName) {
        sh "docker build -t 'docker.io/${image}:${version}' ."
        return
    }

    sh "docker build -t 'docker.io/${image}:${version}-${envName}' ."
}

def tag(
    String image,
    String version = 'latest',
    String envName = ''
) {
    validateArguments(image, version, envName)
    if (version == 'latest') {
        return
    }

    echo 'Tagging Docker image...'

    if (!envName) {
        sh """
            docker tag \
                'docker.io/${image}:${version}' \
                'docker.io/${image}:latest'
        """
        return
    }

    sh """
        docker tag \
            'docker.io/${image}:${version}-${envName}' \
            'docker.io/${image}:latest-${envName}'
    """
}

def login() {
    echo 'Logging in to Docker registry...'

    withCredentials([
        usernamePassword(
            credentialsId:'dockerhub-creds',
            usernameVariable:'DOCKERHUB_USERNAME',
            passwordVariable:'DOCKERHUB_PASSWORD'
        )
    ]) {
        sh '''
            echo "$DOCKERHUB_PASSWORD" | \
                docker login \
                --username "$DOCKERHUB_USERNAME" \
                --password-stdin
        '''
    }
}

def push(
    String image,
    String version = 'latest',
    String envName = ''
) {
    validateArguments(image, version, envName)
    echo 'Pushing Docker image to registry...'

    if (!envName) {
        if (version != 'latest') {
            sh "docker push 'docker.io/${image}:${version}'"
        }

        sh "docker push 'docker.io/${image}:latest'"
        return
    }

    sh """
        docker push 'docker.io/${image}:${version}-${envName}'
        docker push 'docker.io/${image}:latest-${envName}'
    """
}

private void validateArguments(String image, String version, String envName) {
    // Only Docker repository components are allowed, never shell syntax.
    String component = '[a-z0-9]+(?:(?:[._]|__|-+)[a-z0-9]+)*'
    if (!image || !(image ==~ "${component}(?:/${component})*")) {
        throw new IllegalArgumentException('image must be a lowercase Docker repository path')
    }
    if (!version || !(version ==~ '[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}')) {
        throw new IllegalArgumentException('version must be a valid Docker tag (1-128 characters)')
    }
    if (envName == null || (envName && !(envName ==~ '[A-Za-z0-9_][A-Za-z0-9_.-]*'))) {
        throw new IllegalArgumentException('envName must be empty or contain a valid tag suffix')
    }
    if (envName && ("${version}-${envName}".length() > 128 || "latest-${envName}".length() > 128)) {
        throw new IllegalArgumentException('environment-qualified Docker tags must not exceed 128 characters')
    }
}

