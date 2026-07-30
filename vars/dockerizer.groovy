def buildImage(
    String image,
    String version = 'latest',
    String envName = ''
){
    echo 'Building docker image...'
    if (!envName) {
        sh "docker build -t docker.io/${image}:${version} ."
        return 0
    }
    sh "docker build -t docker.io/${image}:${version}-${envName} ."
}

def tag(
    String image,
    String version = "latest",
    String envName = ''
){
    if (version == 'latest') {
        return 0
    }
    echo 'Tagging docker image...'
    if (!envName) {
        sh """
        docker tag \
        docker.io/${image}:${version} \
        docker.io/${image}:latest
        """
        return 0
    }
    sh """
    docker tag \
    docker.io/${image}:${version}-${envName} \
    docker.io/${image}:latest-${envName}
    """
}

def login(){
    echo 'Logging in to Docker registry...'
    withCredentials([
        usernamePassword(
            credentialsId: 'dockerhub-creds',
            usernameVariable: 'DOCKERHUB_USERNAME',
            passwordVariable: 'DOCKERHUB_PASSWORD'
        )
    ]){
        withEnv([
            "DOCKER_USERNAME=${DOCKERHUB_USERNAME}",
            "DOCKER_PASSWORD=${DOCKERHUB_PASSWORD}"
        ]){
            sh '''
            echo "$DOCKER_PASSWORD" | \
            docker login \
            --username "$DOCKER_USERNAME" \
            --password-stdin
            '''
        }
    }
}

def push(
    String image,
    String version = 'latest',
    String envName = ''
){
    echo 'Pushing docker image to registry...'
    if (!envName) {
        if (version != 'latest') {
            sh "docker push docker.io/${image}:${version}"
        }
        sh "docker push  docker.io/${image}:latest"
        return  0
    }
    sh """
    docker push docker.io/${image}:${version}-${envName}
    docker push  docker.io/${image}:latest-${envName}
    """
}
