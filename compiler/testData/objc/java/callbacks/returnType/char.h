#import <Foundation/NSObject.h>

typedef char (*fun_t)();

@interface A : NSObject
+ (void) printOKIfLowercaseA: (fun_t) fun;
@end
