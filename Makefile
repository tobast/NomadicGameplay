TARGET=NomadicGameplay
DIR=nomadicgameplay
PKGPATH=fr/tobast/bukkit/$(DIR)

all:
	@cd $(PKGPATH) && make
	jar cf $(TARGET).jar $(PKGPATH)/*.class plugin.yml LICENCE.txt README.md

clean:
	@cd $(PKGPATH) && make clean

