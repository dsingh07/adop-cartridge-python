// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
// **The git repo variables will be changed to the users' git repositories manually in the Jenkins jobs**
def pythonAppgitRepo = "adop-cartridge-python-reference"
def pythonAppgitURL = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + pythonAppgitRepo

// Jobs
def buildAppJob = freeStyleJob(projectFolderName + "/Python_Build")

// Views
def pipelineView = buildPipelineView(projectFolderName + "/Reference_Python_Application")

pipelineView.with{
    title('Reference Python Application Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + "/Python_Build")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}

// All jobs are tied to build on the Jenkins slave

buildAppJob.with{
	description("Python application build job.")
	scm{
		git{
			remote{
				url(pythonAppgitURL)
				credentials("adop-jenkins-master")
			}
			branch("*/master")
		}
	}
	environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
    }
	label("docker")
	wrappers {
		preBuildCleanup()
		injectPasswords()
		maskPasswords()
		sshAgent("adop-jenkins-master")
	}
	triggers{
		gerrit{
		  events{
			refUpdated()
		  }
		  configure { gerritxml ->
			gerritxml / 'gerritProjects' {
			  'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject' {
				compareType("PLAIN")
				pattern(projectFolderName + "/" + pythonAppgitRepo)
				'branches' {
				  'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.Branch' {
					compareType("PLAIN")
					pattern("master")
				  }
				}
			  }
			}
			gerritxml / serverName("ADOP Gerrit")
		  }
		}
	}
	steps {
		shell('''set +x
		  |# Set the workspace directory to point to the Jenkins slave volume
	      |export docker_workspace_dir=$(echo ${WORKSPACE} | sed 's#/workspace#/var/lib/docker/volumes/jenkins_slave_home/_data#')
	      |
	      |docker run --net=host --rm --name my-running-script  -v /var/run/docker.sock:/var/run/docker.sock -v ${docker_workspace_dir}/:/tmp/scripts/ python:3 python /tmp/scripts/prime_numbers.py
	    '''.stripMargin())
	}
}
