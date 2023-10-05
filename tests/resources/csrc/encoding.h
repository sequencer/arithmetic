struct DutInterface{
                    svBit ready;

                    svBit *valid;
                    svBitVecVal *a;
                    svBitVecVal *b;
                    svBitVecVal *op;
                    svBitVecVal *rm;
                    svBitVecVal *refOut;
                    svBitVecVal *refFlags
};