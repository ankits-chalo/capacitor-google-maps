#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface ObjCExceptionCatcher : NSObject

/// Executes `tryBlock` and returns YES on success.
/// If an ObjC NSException is thrown, it is caught and passed to `catchBlock`; returns NO.
+ (BOOL)tryBlock:(void (NS_NOESCAPE ^)(void))tryBlock
      catchBlock:(void (NS_NOESCAPE ^)(NSException *exception))catchBlock;

@end

NS_ASSUME_NONNULL_END
