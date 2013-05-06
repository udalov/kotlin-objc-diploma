#import <Foundation/NSObject.h>

@interface Printer : NSObject
- (void) print;
@end

@interface OKPrinter : Printer
@end


@interface PrintingServices : NSObject
+ (void) printWithPrinter: (Printer *)printer;
+ (Printer *) getDefaultPrinter;
@end
