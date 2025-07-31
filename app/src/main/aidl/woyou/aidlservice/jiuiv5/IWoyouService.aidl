package woyou.aidlservice.jiuiv5;

import woyou.aidlservice.jiuiv5.ICallback;

interface IWoyouService {
    /**
     * 打印文字
     * Print text
     */
    void printText(String text, in ICallback callback);

    /**
     * 打印指定字体大小文字
     * Print text with specified font size
     */
    void printTextWithFont(String text, in ICallback callback, float size, in ICallback callback2);

    /**
     * 设置对齐模式
     * Set alignment mode
     * 0-左对齐, 1-居中对齐, 2-右对齐
     * 0-left, 1-center, 2-right
     */
    void setAlignment(int alignment, in ICallback callback);

    /**
     * 打印条码
     * Print barcode
     */
    void printBarCode(String data, int symbology, int height, int width, int textposition, in ICallback callback);

    /**
     * 打印二维码
     * Print QR code
     */
    void printQRCode(String data, int modulesize, int errorlevel, in ICallback callback);

    /**
     * 换行
     * Line feed
     */
    void lineWrap(int n, in ICallback callback);

    /**
     * 使用原始指令打印
     * Print using raw commands
     */
    void sendRAWData(in byte[] data, in ICallback callback);

    /**
     * 设置打印浓度
     * Set print density
     */
    void setPrinterStyle(int key, int value);

    /**
     * 获取打印机状态
     * Get printer status
     */
    int getPrinterStatus();

    /**
     * 获取打印机序列号
     * Get printer serial number
     */
    String getPrinterSerialNo();

    /**
     * 获取打印机版本
     * Get printer version
     */
    String getPrinterVersion();

    /**
     * 获取打印机纸张规格
     * Get printer paper spec
     */
    String getPaperSize();

    /**
     * 打印bitmap图片
     * Print bitmap image
     */
    void printBitmap(in android.graphics.Bitmap bitmap, in ICallback callback);

    /**
     * 进入缓冲打印模式
     * Enter buffer print mode
     */
    void enterPrinterBuffer(boolean clean);

    /**
     * 退出缓冲打印模式并打印
     * Exit buffer print mode and print
     */
    void exitPrinterBuffer(boolean commit);
}