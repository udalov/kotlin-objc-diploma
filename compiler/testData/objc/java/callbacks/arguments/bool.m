#import "bool.h"

@implementation A
+ (void) invokeWithTrueAndFalse: (fun_t) fun {
    fun(YES, NO);
}
@end
