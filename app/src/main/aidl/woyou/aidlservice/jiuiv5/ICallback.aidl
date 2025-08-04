package woyou.aidlservice.jiuiv5;

oneway interface ICallback {
    void onRunResult(boolean isSuccess);
    void onReturnString(String result);
    void onRaiseException(int code, String msg);
}