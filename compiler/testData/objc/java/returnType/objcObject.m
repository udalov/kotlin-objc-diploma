#import "objcObject.h"
#import <stdio.h>

@implementation OKPrinter
- (void) printOK {
    printf("OK");
}
@end


@implementation PrinterProvider
+ (OKPrinter *) getPrinter {
    return [[OKPrinter alloc] init];
}
@end
