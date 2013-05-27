#import <Foundation/NSObject.h>

@class Printer;

typedef void (*fun_t)(Printer *);

@interface Printer : NSObject
- (void) printOK;
@end

@interface PrintingServices : NSObject
+ (void) invokeOnPrinter: (fun_t) callback;
@end
