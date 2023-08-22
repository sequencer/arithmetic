init:
	git submodule update --init

compile:
	mill -i -j 0 arithmetic[5.0.0].compile

run:
	mill -i -j 0 arithmetic[5.0.0].run

bsp:
	mill -i mill.bsp.BSP/install

clean:
	git clean -fd

