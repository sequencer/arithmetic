#pragma once

struct DutInterface{
                    svBit *valid;
                    svBitVecVal *a;
                    svBitVecVal *b;
                    svBitVecVal *op;
                    svBitVecVal *rm;
                    svBitVecVal *refOut;
                    svBitVecVal *refFlags;
};
