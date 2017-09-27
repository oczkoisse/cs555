FILES = $(shell find ../src/cs555/a2 -name "*.java")

all: compile
	@echo -e '[INFO] Done!'
clean:
	@echo -e '[INFO] Cleaning Up..'
	@rm -rf ../out/production/cs555/cs555/a2

compile: 
	@echo -e '[INFO] Compiling the Source..'
	@mkdir -p ../out/production/cs555
	@javac -d ../out/production/cs555 $(FILES)
