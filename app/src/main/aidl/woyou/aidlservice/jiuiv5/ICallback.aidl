package woyou.aidlservice.jiuiv5;

interface ICallback {
    /**
     * 操作结果回调
     * Operation result callback
     */
    void onRunResult(boolean isSuccess);

    /**
     * 返回结果回调
     * Return result callback
     */
    void onReturnString(String result);

    /**
     * 异常回调
     * Exception callback
     */
    void onRaiseException(int code, String msg);
}