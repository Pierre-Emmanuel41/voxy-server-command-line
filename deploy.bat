@call cd ..

@rem Downloading dependencies
@if not exist utils (
	@echo Cloning git repo for project utils
    @call git clone --branch 2.0-SNAPSHOT --single-branch https://github.com/Pierre-Emmanuel41/utils.git
) else ( 
	@call cd utils
	
	@echo Pulling latest changes for project utils
	@call git pull
	
	@call cd ..
)

@if not exist protocol (
	@echo Cloning git repo for project protocol
    @call git clone --branch 1.0-SNAPSHOT --single-branch https://github.com/Pierre-Emmanuel41/protocol.git
) else ( 
	@call cd protocol
	
	@echo Pulling latest changes for project protocol
	@call git pull
	
	@call cd ..
)

@if not exist voxy-common (
	@echo Cloning git repo for project voxy-common
    @call git clone --branch 1.0-SNAPSHOT --single-branch https://github.com/Pierre-Emmanuel41/voxy-common.git
) else ( 
	@call cd voxy-common
	
	@echo Pulling latest changes for project voxy-common
	@call git pull
	
	@call cd ..
)

@if not exist communication (
	@echo Cloning git repo for project communication
    @call git clone --branch 2.0-SNAPSHOT --single-branch https://github.com/Pierre-Emmanuel41/communication.git
) else (
	@call cd communication

	@echo Pulling latest changes for project protocol
	@call git pull

	@call cd ..
)

@if not exist messenger (
	@echo Cloning git repo for project messenger
    @call git clone --branch 2.0-SNAPSHOT --single-branch https://github.com/Pierre-Emmanuel41/messenger.git
) else (
	@call cd messenger

	@echo Pulling latest changes for project messenger
	@call git pull

	@call cd ..
)

@if not exist voxy-server (
	@echo Cloning git repo for project messenger
    @call git clone --branch 1.0-SNAPSHOT --single-branch https://github.com/Pierre-Emmanuel41/voxy-server.git
) else (
	@call cd voxy-server

	@echo Pulling latest changes for project voxy-server
	@call git pull

	@call cd ..
)

@rem Building dependencies
@echo Building project utils
@call cd utils
@call mvn clean package install
@call cd ..

@echo Building project protocol
@call cd protocol
@call mvn clean package install
@call cd ..

@echo Building project voxy-common
@call cd voxy-common
@call mvn clean package install
@call cd ..

@echo Building project communication
@call cd communication
@call mvn clean package install
@call cd ..

@echo Building project messenger
@call cd messenger
@call mvn clean package install
@call cd ..

@echo Building project voxy-server
@call cd voxy-server
@call mvn clean package install
@call cd ..

@echo Building project voxy-server-command-line
@call cd voxy-server-command-line
@call mvn clean package install
@call cd ..