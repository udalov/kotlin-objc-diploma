#import <Foundation/NSObject.h>

@interface Printer : NSObject
- (void) print;
@end

@interface OKPrinter : Printer
@end

@interface FailPrinter : Printer
@end


@interface PrinterProvider : NSObject
+ (Printer *) getPrinter;
@end
