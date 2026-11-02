// Animation-only history ownership. Core tool and history implementations stay unchanged.
#include <map>
namespace {
struct AnimationFrameHistory {
  std::vector<Snapshot> undo, redo;
  std::shared_ptr<velyntora::history::HistoryArchive> archive;
  std::uint64_t revision;
  int tag;
};
std::map<std::string, AnimationFrameHistory> animationHistories;
std::string animationHistoryKey(JNIEnv* env, jstring key) {
  const char* chars=env->GetStringUTFChars(key,nullptr);
  if(!chars) return {};
  std::string value(chars);env->ReleaseStringUTFChars(key,chars);return value;
}
}
extern "C" JNIEXPORT void JNICALL
Java_art_velyntora_core_FrameDocumentAdapter_saveHistory(JNIEnv* env,jclass,jstring key) {
  std::lock_guard<std::mutex> lock(guard);
  try {animationHistories.insert_or_assign(animationHistoryKey(env,key),
    AnimationFrameHistory{undoStack,redoStack,historyArchive,revision,curveTag});}
  catch(const std::exception& e){env->ThrowNew(env->FindClass("java/io/IOException"),e.what());}
}
extern "C" JNIEXPORT void JNICALL
Java_art_velyntora_core_FrameDocumentAdapter_restoreHistory(JNIEnv* env,jclass,jstring key) {
  std::lock_guard<std::mutex> lock(guard);
  try {
    auto found=animationHistories.find(animationHistoryKey(env,key));
    if(found==animationHistories.end())return;
    auto undo=found->second.undo,redo=found->second.redo;
    undoStack=std::move(undo);redoStack=std::move(redo);
    historyArchive=found->second.archive;revision=found->second.revision;curveTag=found->second.tag;
  } catch(const std::exception& e){env->ThrowNew(env->FindClass("java/io/IOException"),e.what());}
}
extern "C" JNIEXPORT void JNICALL
Java_art_velyntora_core_FrameDocumentAdapter_dropHistory(JNIEnv* env,jclass,jstring key) {
  std::lock_guard<std::mutex> lock(guard);
  animationHistories.erase(animationHistoryKey(env,key));
}
extern "C" JNIEXPORT void JNICALL
Java_art_velyntora_core_FrameDocumentAdapter_clearHistories(JNIEnv*,jclass) {
  std::lock_guard<std::mutex> lock(guard);animationHistories.clear();
}
