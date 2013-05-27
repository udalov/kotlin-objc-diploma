#import "objcObject.h"

#import <stdio.h>

@implementation Printer
+ (Printer *) instance {
    return [[Printer alloc] init];
}

- (void) printOK {
    printf("OK");
}

+ (void) printOKWithLazyInstance: (fun_t) lazyPrinter {
    Printer *printer = lazyPrinter();
    [printer printOK];
}
@end
