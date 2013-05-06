#import "objcObjectIsCheck.h"
#import <stdio.h>

@implementation Printer
- (void) print {
    printf("Generic printer");
}
@end

@implementation OKPrinter
- (void) print {
    printf("OK");
}
@end

@implementation FailPrinter
- (void) print {
    printf("Fail");
}
@end


@implementation PrinterProvider
+ (Printer *) getPrinter {
    static int times = 0;
    if (times++ < 5) {
        return [[FailPrinter alloc] init];
    } else {
        return [[OKPrinter alloc] init];
    }
}
@end
