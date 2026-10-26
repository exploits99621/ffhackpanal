\
#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <string>
#include <mutex>
#include <thread>
#include <atomic>
#include <chrono>
#include <sys/socket.h>
#include <netdb.h>
#include <unistd.h>

#define TAG "FFHACK"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static const char* kVerifyUrl = "http://example.invalid/api/verify";
static const char* kVersion   = "1.0.0";
static const char* kTelegram  = "https://t.me/blackbanner999";
static const char* kYoutube   = "https://youtube.com/@blackbanner999";

struct Ctl {
    const char* label; int mode; int mn, mx, def; const char* unit; bool on;
};
struct State {
    std::mutex mtx;
    std::string key;
    int verified = 0;
    Ctl ctls[10] = {
        {"ESP Box",       0, 0, 1,   0, "",     false},
        {"ESP Line",      0, 0, 1,   0, "",     false},
        {"ESP Distance",  0, 0, 1,   1, "",     true },
        {"ESP Health",    0, 0, 1,   1, "",     true },
        {"Team Filter",   0, 0, 1,   1, "",     true },
        {"Visible Check", 0, 0, 1,   1, "",     true },
        {"Aim FOV",       1, 40, 400, 120, "px", false},
        {"Aim Smooth",    1, 1, 20,  6,  "x",   false},
        {"Target Chest",  0, 0, 1,   1, "",     true },
        {"Triggerbot",    0, 0, 1,   0, "",     false},
    };
};
static State g;

static std::string jstr(JNIEnv* e, jstring s) {
    if (!s) return {};
    const char* p = e->GetStringUTFChars(s, nullptr);
    std::string out(p ? p : "");
    e->ReleaseStringUTFChars(s, p);
    return out;
}
static std::string sign_key(const std::string& k) {
    unsigned sum = 0;
    for (unsigned char c : k) sum = sum * 31 + c;
    char b[16]; snprintf(b, sizeof b, "%08x", sum);
    return b;
}

static std::string http_get(const char* url) {
    // simple GET, no TLS, matches original .so's socket/send/recv pattern.
    const char* p = url;
    if (!strncmp(p, "http://", 7)) p += 7; else return {};
    const char* slash = strchr(p, '/');
    std::string host = slash ? std::string(p, slash - p) : std::string(p);
    std::string path = slash ? std::string(slash) : "/";
    struct addrinfo hints{}, *res = nullptr;
    hints.ai_family = AF_INET; hints.ai_socktype = SOCK_STREAM;
    if (getaddrinfo(host.c_str(), "80", &hints, &res) != 0) return {};
    int s = socket(res->ai_family, res->ai_socktype, 0);
    if (s < 0) { freeaddrinfo(res); return {}; }
    timeval tv{5,0};
    setsockopt(s, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof tv);
    if (connect(s, res->ai_addr, res->ai_addrlen) != 0) { close(s); freeaddrinfo(res); return {}; }
    freeaddrinfo(res);
    char req[1024];
    int rl = snprintf(req, sizeof req,
        "GET %s HTTP/1.1\r\nHost: %s\r\nUser-Agent: FFHACK/%s\r\nConnection: close\r\n\r\n",
        path.c_str(), host.c_str(), kVersion);
    send(s, req, rl, 0);
    std::string out; char buf[1024]; ssize_t n;
    while ((n = recv(s, buf, sizeof buf, 0)) > 0) out.append(buf, n);
    close(s);
    auto pos = out.find("\r\n\r\n");
    return pos == std::string::npos ? out : out.substr(pos + 4);
}

extern "C" {

JNIEXPORT void JNICALL Java_com_ffhack_HunterNative_nInit(JNIEnv*, jobject) { LOGI("init"); }

JNIEXPORT jint JNICALL Java_com_ffhack_HunterNative_nConfirmCode(JNIEnv* e, jobject, jstring k) {
    std::string key = jstr(e, k);
    if (key.size() < 8) return -1;
    std::string url = std::string(kVerifyUrl) + "?key=" + key + "&sig=" + sign_key(key);
    std::string r;
    if (strstr(kVerifyUrl, "invalid")) {           // offline mode for demo
        std::lock_guard<std::mutex> lk(g.mtx);
        g.key = key; g.verified = 1; return 0;
    }
    r = http_get(url.c_str());
    std::lock_guard<std::mutex> lk(g.mtx);
    if (r.find("ok") != std::string::npos) { g.key = key; g.verified = 1; return 0; }
    return -2;
}
JNIEXPORT jstring JNICALL Java_com_ffhack_HunterNative_nGetKey(JNIEnv* e, jobject) {
    std::lock_guard<std::mutex> lk(g.mtx); return e->NewStringUTF(g.key.c_str());
}
JNIEXPORT jstring JNICALL Java_com_ffhack_HunterNative_nSign(JNIEnv* e, jobject, jstring k) {
    return e->NewStringUTF(sign_key(jstr(e, k)).c_str());
}
JNIEXPORT jstring JNICALL Java_com_ffhack_HunterNative_nVersion(JNIEnv* e, jobject) {
    return e->NewStringUTF(kVersion);
}
JNIEXPORT jstring JNICALL Java_com_ffhack_HunterNative_nTelegram(JNIEnv* e, jobject) {
    return e->NewStringUTF(kTelegram);
}
JNIEXPORT jstring JNICALL Java_com_ffhack_HunterNative_nYoutube(JNIEnv* e, jobject) {
    return e->NewStringUTF(kYoutube);
}

JNIEXPORT void JNICALL Java_com_ffhack_HunterNative_nInitOverlay(JNIEnv*, jobject, jint, jint) {}
JNIEXPORT void JNICALL Java_com_ffhack_HunterNative_nSetLicense(JNIEnv* e, jobject, jstring k) {
    std::lock_guard<std::mutex> lk(g.mtx); g.key = jstr(e, k); g.verified = 1;
}

JNIEXPORT jint JNICALL Java_com_ffhack_HunterNative_nDraw(
    JNIEnv* e, jobject, jint w, jint h,
    jintArray boxJ, jfloatArray lineJ, jfloatArray targetJ,
    jfloatArray distJ, jfloatArray hpJ)
{
    if (g.verified != 1) return 0;
    if (!g.ctls[0].on && !g.ctls[1].on && !g.ctls[2].on && !g.ctls[3].on) return 0;

    // Placeholder target — replace with entity-list iteration when wired to
    // the game's libil2cpp.so (Il2CppDumper offsets needed).
    int bw = w / 6, bh = h / 4;
    int cx = w / 2, cy = h / 2;
    int b[4] = { cx - bw/2, cy - bh/2, cx + bw/2, cy + bh/2 };
    e->SetIntArrayRegion(boxJ, 0, 4, b);

    float l[4] = { (float)w/2, (float)h, (float)(w/2), (float)(cy - bh/2) };
    e->SetFloatArrayRegion(lineJ, 0, 4, l);

    float t[2] = { (float)cx, (float)(cy - bh/2) };
    e->SetFloatArrayRegion(targetJ, 0, 2, t);

    float d[1] = { 42.0f };
    e->SetFloatArrayRegion(distJ, 0, 1, d);
    float hpf[1] = { 0.8f };
    e->SetFloatArrayRegion(hpJ, 0, 1, hpf);
    return 1;
}

JNIEXPORT jint JNICALL Java_com_ffhack_HunterNative_nGetControlCount(JNIEnv*, jobject) { return 10; }

JNIEXPORT jstring JNICALL Java_com_ffhack_HunterNative_nGetControlLabel(JNIEnv* e, jobject, jint i) {
    if (i < 0 || i >= 10) return e->NewStringUTF("");
    return e->NewStringUTF(g.ctls[i].label);
}
JNIEXPORT jint JNICALL Java_com_ffhack_HunterNative_nGetControlMode(JNIEnv*, jobject, jint i) {
    return (i < 0 || i >= 10) ? 0 : g.ctls[i].mode;
}
JNIEXPORT jint JNICALL Java_com_ffhack_HunterNative_nGetSliderMin(JNIEnv*, jobject, jint i) {
    return (i < 0 || i >= 10) ? 0 : g.ctls[i].mn;
}
JNIEXPORT jint JNICALL Java_com_ffhack_HunterNative_nGetSliderMax(JNIEnv*, jobject, jint i) {
    return (i < 0 || i >= 10) ? 1 : g.ctls[i].mx;
}
JNIEXPORT jint JNICALL Java_com_ffhack_HunterNative_nGetSliderDefault(JNIEnv*, jobject, jint i) {
    return (i < 0 || i >= 10) ? 0 : g.ctls[i].def;
}
JNIEXPORT jstring JNICALL Java_com_ffhack_HunterNative_nGetSliderUnit(JNIEnv* e, jobject, jint i) {
    return e->NewStringUTF((i < 0 || i >= 10) ? "" : g.ctls[i].unit);
}
JNIEXPORT jboolean JNICALL Java_com_ffhack_HunterNative_nGetStatus(JNIEnv*, jobject, jint i) {
    return (i < 0 || i >= 10) ? JNI_FALSE : (g.ctls[i].on ? JNI_TRUE : JNI_FALSE);
}
JNIEXPORT void JNICALL Java_com_ffhack_HunterNative_nSendToggle(JNIEnv*, jobject, jint i, jboolean on) {
    if (i < 0 || i >= 10) return;
    std::lock_guard<std::mutex> lk(g.mtx);
    g.ctls[i].on = (on == JNI_TRUE);
}
JNIEXPORT void JNICALL Java_com_ffhack_HunterNative_nSendSlider(JNIEnv*, jobject, jint i, jint v) {
    if (i < 0 || i >= 10) return;
    std::lock_guard<std::mutex> lk(g.mtx);
    g.ctls[i].def = v;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) {
    LOGI("FFHACK loaded");
    return JNI_VERSION_1_6;
}

} // extern "C"
