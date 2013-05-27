#import "double.h"

@implementation A
+ (void) invokeWithPi: (fun_t) fun {
    fun(3.14159265358979);
}
@end
