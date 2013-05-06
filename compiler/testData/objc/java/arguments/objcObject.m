#import "objcObject.h"
#import "stdio.h"

@implementation Printer
- (void) print {
    printf("Printer");
}
@end

@implementation OKPrinter
- (void) print {
    printf("OK");
}
@end


@implementation PrintingServices
+ (void) printWithPrinter: (Printer *)printer {
    [printer print];
}

+ (Printer *) getDefaultPrinter {
    return [[OKPrinter alloc] init];
}
@end

