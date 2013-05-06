#import <Foundation/NSObject.h>

@interface OKPrinter : NSObject
- (void) printOK;
@end


@interface PrinterProvider : NSObject
+ (OKPrinter *) getPrinter;
@end
