TARGET=NomadicGameplay
DIR=nomadicgameplay
PKGPATH=fr/tobast/bukkit/$(DIR)

all:
	@cd $(PKGPATH) && make
	jar cf $(TARGET).jar $(PKGPATH)/*.class plugin.yml licence.txt

clean:
	@cd $(PKGPATH) && make clean

