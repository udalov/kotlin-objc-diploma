#import "char.h"

@implementation A
+ (void) invokeWithABC: (fun_t) fun {
    fun('A', 'B', 'C');
}
@end
