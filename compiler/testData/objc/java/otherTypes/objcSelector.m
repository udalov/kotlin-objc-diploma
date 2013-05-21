#import "objcSelector.h"

#import <objc/message.h>
#import <stdio.h>

@implementation A
+ (SEL) getSelector {
    return @selector(printOK);
}

+ (void) invokeSelector: (SEL) selector {
    objc_msgSend(self, selector);
}

+ (void) printOK {
    printf("OK");
}
@end


