#include <jni.h>
#include <cmath>

extern "C"
JNIEXPORT void JNICALL
Java_com_asees_imageclassifier_1q2_MainActivity_preprocessImage(JNIEnv *env, jobject thiz,
                                                                jbyteArray img_data) {
    // Get the image data from Java byte array
    jbyte *imgData = env->GetByteArrayElements(img_data, NULL);
    if (imgData == NULL) {
        return; // Return if image data is NULL
    }

    // Assuming the image is in RGBA format with 4 channels (Red, Green, Blue, Alpha)
    int imageSize = env->GetArrayLength(img_data);
    int width = sqrt(imageSize / 4); // Calculate the width of the image
    int height = width; // Assuming the image is square

    // Loop through each pixel in the image
    for (int i = 0; i < imageSize; i += 4) {
        // Extract RGBA values of the pixel
        int red = imgData[i] & 0xFF;
        int green = imgData[i + 1] & 0xFF;
        int blue = imgData[i + 2] & 0xFF;
        int alpha = imgData[i + 3] & 0xFF;

        // Example preprocess: Convert RGB to grayscale
        int gray = (red + green + blue) / 3;

        // Update the pixel values with the preprocessed value
        imgData[i] = gray;
        imgData[i + 1] = gray;
        imgData[i + 2] = gray;
        // Leave alpha channel unchanged
    }

    // Release the image data
    env->ReleaseByteArrayElements(img_data, imgData, 0);
}
