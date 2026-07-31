def buildImage(
    String image,
    String version = 'latest',
    String envName = ''
) {
    echo 'Building Docker image...'

    if (!envName) {
        sh "docker build -t docker.io/${image}:${version} ."
        return
    }

    sh "docker build -t docker.io/${image}:${version}-${envName} ."
}

def tag(
    String image,
    String version = 'latest',
    String envName = ''
) {
    if (version == 'latest') {
        return
    }

    echo 'Tagging Docker image...'

    if (!envName) {
        sh """
            docker tag \
                docker.io/${image}:${version} \
                docker.io/${image}:latest
        """
        return
    }

    sh """
        docker tag \
            docker.io/${image}:${version}-${envName} \
            docker.io/${image}:latest-${envName}
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
    echo 'Pushing Docker image to registry...'

    if (!envName) {
        if (version != 'latest') {
            sh "docker push docker.io/${image}:${version}"
        }

        sh "docker push docker.io/${image}:latest"
        return
    }

    sh """
        docker push docker.io/${image}:${version}-${envName}
        docker push docker.io/${image}:latest-${envName}
    """
}
