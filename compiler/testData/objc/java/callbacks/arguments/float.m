#import "float.h"

@implementation A
+ (void) invokeWithPi: (fun_t) fun {
    fun(3.14159265f);
}
@end
