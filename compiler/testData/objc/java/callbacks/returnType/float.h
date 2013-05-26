#import <Foundation/NSObject.h>

typedef float (*fun_t)();

@interface A : NSObject
+ (void) printOKIfPi: (fun_t) fun;
@end
