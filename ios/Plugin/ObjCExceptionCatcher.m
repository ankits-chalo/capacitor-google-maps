#import "ObjCExceptionCatcher.h"

@implementation ObjCExceptionCatcher

+ (BOOL)tryBlock:(void (NS_NOESCAPE ^)(void))tryBlock
      catchBlock:(void (NS_NOESCAPE ^)(NSException *exception))catchBlock {
    @try {
        tryBlock();
        return YES;
    } @catch (NSException *exception) {
        catchBlock(exception);
        return NO;
    }
}

@end
