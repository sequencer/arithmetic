init:
	git submodule update --init

compile:
	mill -i arithmetic[5.0.0].compile

bsp:
	mill -i mill.bsp.BSP/install

clean:
	git clean -fd

