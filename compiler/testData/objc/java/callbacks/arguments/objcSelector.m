#import "objcSelector.h"

#import <stdio.h>

@implementation A : NSObject
+ (void) invoke: (fun_t) callback {
    callback(@selector(invoke:));
}

+ (void) checkIfEqualsToInvoke: (SEL) selector {
    if (selector == @selector(invoke:))
        printf("OK");
    else
        printf("Fail");
}
@end
