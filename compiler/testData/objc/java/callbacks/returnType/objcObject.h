#import <Foundation/NSObject.h>

@class Printer;

typedef Printer* (*fun_t)();

@interface Printer : NSObject
+ (Printer *) instance;

- (void) printOK;

+ (void) printOKWithLazyInstance: (fun_t) lazyPrinter;
@end
