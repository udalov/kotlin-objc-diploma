#import "pointer.h"

#import <stdio.h>

@implementation A : NSObject
+ (void) invoke: (fun_t) callback {
    callback(self);
}

+ (void) checkIfEqualsToThis: (void *) pointer {
    if (pointer == self)
        printf("OK");
    else
        printf("Fail");
}
@end
