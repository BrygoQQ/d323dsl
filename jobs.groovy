def student = 'akiryushin'
def git = 'BrygoQQ/d323dsl'
def parGitUrl = "https://github.com/${git}"
def branch = '*/${BRANCH}'
def quantity = 4
def command = "git ls-remote -h $parGitUrl"

def proc = command.execute()
proc.waitFor()

if ( proc.exitValue() != 0 ) {
    println "Error, ${proc.err.text}"
    System.exit(-1)
}

def branches = proc.in.text.readLines().collect {
    it.replaceAll(/[a-z0-9]*\trefs\/heads\//, '')
}

println('Create main job')
job("MNTLAB-${student}-main-build-job") {
    description('Main job')
	parameters {
		choiceParam('BRANCH', ['akiryushin', 'master'], 'Change branch')
		activeChoiceParam('Jobs') {
            description('Choose Jobs')
            choiceType('CHECKBOX')
            groovyScript {
                script('list = []\nfor(i in 1..4) {\nlist.add("MNTLAB-akiryushin-child${i}-build-job")\n}\nreturn list')
            }
        }
	}
	
	steps {
		downstreamParameterized {
            trigger('$Jobs') {
                block {
                    buildStepFailure('FAILURE')
                    failure('FAILURE')
                    unstable('UNSTABLE')
                }
                parameters {
                    currentBuild()
                }
            }
        }
	}
	
	for (int i=1; i <= quantity; i++) {
			
		
		job("MNTLAB-${student}-child${i}-build-job") {
			description("Child build ${i}")
			scm {
				
				github(git, branch)
				
            }
            parameters { choiceParam('BRANCH', branches) }
            steps {
                shell('chmod +x ./script.sh\n./script.sh >> output.txt\ntar -czf ${BRANCH}_dsl_script.tar.gz output.txt script.sh')
            }
			publishers {
				archiveArtifacts {
					pattern('${BRANCH}_dsl_script.tar.gz')
					onlyIfSuccessful()
				}
			}
		}
	}
}
