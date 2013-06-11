CMP= javac
LIBS= -cp /usr/share/java/bukkitapi-latest.jar
FLAGS=-Xlint:unchecked -Xlint:deprecation -target 1.6 -source 1.6

all: targets
	$(CMP) $(LIBS) $(FLAGS) @sourcefiles
	@rm sourcefiles

targets:
	@echo "" > sourcefiles
	@ls | grep -e .java$ >> sourcefiles

clean:
	rm -f *.class 
