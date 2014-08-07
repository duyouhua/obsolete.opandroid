/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_openpeer_javaapi_OPConversationThreadSystemMessage */

#ifndef _Included_com_openpeer_javaapi_OPConversationThreadSystemMessage
#define _Included_com_openpeer_javaapi_OPConversationThreadSystemMessage
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_openpeer_javaapi_OPConversationThreadSystemMessage
 * Method:    toString
 * Signature: (Lcom/openpeer/javaapi/SystemMessageTypes;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_openpeer_javaapi_OPConversationThreadSystemMessage_toString
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_openpeer_javaapi_OPConversationThreadSystemMessage
 * Method:    toSystemMessageType
 * Signature: (Ljava/lang/String;)Lcom/openpeer/javaapi/SystemMessageTypes;
 */
JNIEXPORT jobject JNICALL Java_com_openpeer_javaapi_OPConversationThreadSystemMessage_toSystemMessageType
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_openpeer_javaapi_OPConversationThreadSystemMessage
 * Method:    getMessageType
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_openpeer_javaapi_OPConversationThreadSystemMessage_getMessageType
  (JNIEnv *, jclass);

/*
 * Class:     com_openpeer_javaapi_OPConversationThreadSystemMessage
 * Method:    createCallMessage
 * Signature: (Lcom/openpeer/javaapi/SystemMessageTypes;Lcom/openpeer/javaapi/OPContact;I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_openpeer_javaapi_OPConversationThreadSystemMessage_createCallMessage
  (JNIEnv *, jclass, jobject, jobject, jint);

/*
 * Class:     com_openpeer_javaapi_OPConversationThreadSystemMessage
 * Method:    sendMessage
 * Signature: (Lcom/openpeer/javaapi/OPConversationThread;Ljava/lang/String;Ljava/lang/String;Z)V
 */
JNIEXPORT void JNICALL Java_com_openpeer_javaapi_OPConversationThreadSystemMessage_sendMessage
  (JNIEnv *, jclass, jobject, jstring, jstring, jboolean);

/*
 * Class:     com_openpeer_javaapi_OPConversationThreadSystemMessage
 * Method:    parseAsSystemMessage
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Lcom/openpeer/javaapi/OPParsedSystemMessage;
 */
JNIEXPORT jobject JNICALL Java_com_openpeer_javaapi_OPConversationThreadSystemMessage_parseAsSystemMessage
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     com_openpeer_javaapi_OPConversationThreadSystemMessage
 * Method:    getCallMessageInfo
 * Signature: (Lcom/openpeer/javaapi/OPConversationThread;Ljava/lang/String;)Lcom/openpeer/javaapi/OPCallMessageInfo;
 */
JNIEXPORT jobject JNICALL Java_com_openpeer_javaapi_OPConversationThreadSystemMessage_getCallMessageInfo
  (JNIEnv *, jclass, jobject, jstring);

#ifdef __cplusplus
}
#endif
#endif