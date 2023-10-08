

init:
	git submodule update --init

compile:
	mill -i -j 0 arithmetic[5.0.0].compile

run:
	mill -i -j 0 arithmetic[5.0.0].run

test:
	mill -i -j 0 arithmetictest[5.0.0].test

bsp:
	mill -i mill.bsp.BSP/install

clean:
	git clean -fd

softfloat:
	make -C berkeley-softfloat-3/build/Linux-x86_64-GCC  SPECIALIZE_TYPE=RISCV TESTFLOAT_OPTS="-DFLOAT64 -DFLOAT_ROUND_ODD" softfloat.a -j `nproc`
	cp berkeley-softfloat-3/build/Linux-x86_64-GCC/softfloat.a run/

testfloat:
	make -C berkeley-testfloat-3/build/Linux-x86_64-GCC  SPECIALIZE_TYPE=RISCV TESTFLOAT_OPTS="-DFLOAT64 -DFLOAT_ROUND_ODD" testfloat.a -j `nproc`
	cp berkeley-testfloat-3/build/Linux-x86_64-GCC/testfloat.a run/

