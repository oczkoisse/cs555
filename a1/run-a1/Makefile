all: compile
	@echo -e '[INFO] Done!'
clean:
	@echo -e '[INFO] Cleaning Up..'
	@rm -rf out/production/cs555/cs555/**/**/*.class

compile: 
	@echo -e '[INFO] Compiling the Source..'
	@mkdir -p out/production/cs555
	@javac -d out/production/cs555 src/cs555/**/**/*.java
