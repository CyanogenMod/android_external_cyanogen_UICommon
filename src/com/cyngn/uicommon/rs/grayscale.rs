#pragma version(1)
#pragma rs java_package_name(com.cyngn.uicommon.rs)
#pragma rs_fp_imprecise

uchar4 __attribute__((kernel)) grayscale(uchar4 pixelIn, uint32_t x, uint32_t y) {
 
    uchar grayscale = pixelIn.r * 0.299f + pixelIn.g * 0.587f + pixelIn.b * 0.114f;
    uchar4 pixelOut;

    pixelOut.a = pixelIn.a;
    pixelOut.r = grayscale;
    pixelOut.g = grayscale;
    pixelOut.b = grayscale;

    return pixelOut;
}
