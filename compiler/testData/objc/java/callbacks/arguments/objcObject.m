#import "objcObject.h"

#import <stdio.h>

@implementation Printer
- (void) printOK {
    printf("OK");
}
@end

@implementation PrintingServices
+ (void) invokeOnPrinter: (fun_t) callback {
    Printer *printer = [[[Printer alloc] init] autorelease];
    callback(printer);
}
@end
