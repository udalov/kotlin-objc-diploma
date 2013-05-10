#import "simple.h"

@implementation A
+ (void) invoke: (fun_t) fun {
    fun();
}
@end
