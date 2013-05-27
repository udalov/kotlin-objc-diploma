#import "short.h"

@implementation A
+ (void) invokeWith42: (fun_t) fun {
    fun(42);
}
@end
